package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DifficultyFilter
import com.example.data.model.QuestionItem
import com.example.data.model.SubjectCatalog
import com.example.ui.components.DifficultyFilterBar
import com.example.ui.components.MetricTile
import com.example.ui.theme.AmberHighlight
import com.example.ui.theme.CobaltLight
import com.example.ui.theme.EmeraldCorrect
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.RoseWrong
import com.example.ui.theme.TealSecondary
import com.example.ui.viewmodel.DashboardDerivedState
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.SubjectHubSection
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: DashboardDerivedState,
    difficultyFilter: DifficultyFilter,
    onSelectDifficulty: (DifficultyFilter) -> Unit,
    onLaunchFullMock: () -> Unit,
    onOpenTimedPractice: () -> Unit,
    onOpenSubjectSection: (SubjectHubSection) -> Unit,
    onSelectTab: (MainTab) -> Unit,
    onOpenImporter: () -> Unit,
    onOpenPdfExport: () -> Unit,
    onStartDrill: (List<QuestionItem>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val all = state.questions
    val diffFiltered = difficultyFilter.difficulty?.let { d ->
        all.filter { it.question.difficulty == d }
    } ?: all
    val filteredMistakes = diffFiltered.filter { it.lastOutcome == "wrong" }
    val filteredLowest = difficultyFilter.difficulty?.let { d ->
        state.lowestAccuracyQuestions.filter { it.question.difficulty == d }
    } ?: state.lowestAccuracyQuestions
    val diffLabelSuffix = difficultyFilter.difficulty?.let { " • ${it.label}" } ?: ""
    val pyqCount = all.count { it.question.category == "pyq" || it.question.shiftYear.isNotBlank() }
    val nurCount = all.count { SubjectCatalog.isNursing(it.question) }
    val caCount = all.count { SubjectCatalog.isCa(it.question) }
    val genCount = all.count { SubjectCatalog.isGen(it.question) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> 3
            maxWidth >= 560.dp -> 2
            else -> 1
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1040.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Telemetry Banner
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner_1794982204605),
                            contentDescription = "Clinical ECG telemetry banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xF0090D16),
                                            Color(0xDB0F172A),
                                            Color(0xB31E3A8A)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = Color(0xFF3B82F6).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "RRB Nursing Superintendent & Staff Nurse",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFBFDBFE),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Personal CBT Mastery Hub",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "70:30 Exam Weightage • +1 / -0.33 Negative Marking • 100% Offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onLaunchFullMock,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("launch_100q_mock_btn")
                                ) {
                                    Text("100Q CBT Mock", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.widthIn(min = 6.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.height(16.dp)
                                    )
                                }
                                OutlinedButton(
                                    onClick = onOpenTimedPractice,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.testTag("open_timed_practice_btn")
                                ) {
                                    Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.height(16.dp))
                                    Spacer(modifier = Modifier.widthIn(min = 6.dp))
                                    Text("Timed Drill", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // 4 KPI Tiles
                            val netFormatted = String.format(Locale.US, "%.2f", state.overall.netPracticeScore)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricTile(
                                    label = "Attempted",
                                    value = "${state.overall.attemptedUnique}",
                                    subValue = "/ ${state.overall.totalQuestions}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    label = "Accuracy",
                                    value = "${state.overall.accuracyPercent}%",
                                    valueColor = Color(0xFF6EE7B7),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricTile(
                                    label = "Net Score",
                                    value = netFormatted,
                                    valueColor = Color(0xFF7DD3FC),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    label = "Revision Deck",
                                    value = "${state.overall.bookmarkCount}",
                                    subValue = "saved",
                                    valueColor = Color(0xFFFCD34D),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Focus Drills
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Quick Focus Drills", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Filter by difficulty (${diffFiltered.size} Qs) & target weak areas or rapid mixed sets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        DifficultyFilterBar(
                            selected = difficultyFilter,
                            onSelect = onSelectDifficulty,
                            testTagPrefix = "dash_diff"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onStartDrill(
                                        filteredMistakes,
                                        "Mistakes Review$diffLabelSuffix"
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.testTag("drill_mistakes_btn")
                            ) {
                                Text("Mistakes (${filteredMistakes.size})")
                            }
                            Button(
                                onClick = {
                                    onStartDrill(
                                        filteredLowest.take(25),
                                        "Lowest Accuracy Drill$diffLabelSuffix"
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                modifier = Modifier.testTag("drill_lowest_acc_btn")
                            ) {
                                Text("Lowest Accuracy (${filteredLowest.size})")
                            }
                            Button(
                                onClick = {
                                    onStartDrill(
                                        diffFiltered.shuffled().take(25),
                                        "25Q Rapid Mixed Drill$diffLabelSuffix"
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("drill_25q_random_btn")
                            ) {
                                Text("25Q Random$diffLabelSuffix")
                            }
                        }
                    }
                }
            }

            // Dedicated Modules Grid
            item {
                Text("Study Modules", style = MaterialTheme.typography.titleLarge)
            }

            val moduleCards = listOf(
                ModuleCardData(
                    badge = "2015–2026 Shifts",
                    countLabel = "$pyqCount Qs",
                    title = "Previous Year Questions",
                    desc = "2025 Shifts 1, 2, 3 • 2026 • 2019 • 2015 solved papers with clinical rationales.",
                    badgeColor = AmberHighlight,
                    icon = Icons.Filled.History,
                    tag = "module_pyq",
                    onClick = { onOpenSubjectSection(SubjectHubSection.PYQ) }
                ),
                ModuleCardData(
                    badge = "70% Weightage",
                    countLabel = "$nurCount Qs",
                    title = "Subject-Wise Nursing",
                    desc = "MSN, Pharmacology, OBG, Peds, CHN, Psychiatry, FON, Anatomy & Admin.",
                    badgeColor = RoseWrong,
                    icon = Icons.Filled.Favorite,
                    tag = "module_nursing",
                    onClick = { onOpenSubjectSection(SubjectHubSection.NURSING) }
                ),
                ModuleCardData(
                    badge = "2025–26 + Cards",
                    countLabel = "$caCount Qs + ${state.flashcards.size}",
                    title = "Current Affairs & Static GK",
                    desc = "Union Budget 2026–27, Kavach 4.0, HPV Drive, Padma/Nobel/Sports & Polity.",
                    badgeColor = CobaltLight,
                    icon = Icons.Filled.Public,
                    tag = "module_ca",
                    onClick = { onOpenSubjectSection(SubjectHubSection.CA_GK) }
                ),
                ModuleCardData(
                    badge = "30% Non-Tech",
                    countLabel = "$genCount Qs",
                    title = "Science, Math & Reasoning",
                    desc = "NCERT Physics/Chem/Bio, dosage & aptitude formulas, worked reasoning.",
                    badgeColor = TealSecondary,
                    icon = Icons.Filled.Calculate,
                    tag = "module_gen",
                    onClick = { onOpenSubjectSection(SubjectHubSection.NON_TECH) }
                ),
                ModuleCardData(
                    badge = "Configurable Timer",
                    countLabel = "15 / 30 / 50 Qs",
                    title = "Timed Practice & CBT",
                    desc = "Select any subject or mixed set with custom minutes, or full TCS iON CBT.",
                    badgeColor = EmeraldCorrect,
                    icon = Icons.Filled.Quiz,
                    tag = "module_cbt",
                    onClick = { onSelectTab(MainTab.CBT) }
                ),
                ModuleCardData(
                    badge = "Subject & Category",
                    countLabel = "Analytics",
                    title = "Detailed Performance Stats",
                    desc = "Track accuracy by subject & category and review lowest accuracy questions.",
                    badgeColor = Color(0xFF7C3AED),
                    icon = Icons.Filled.Analytics,
                    tag = "module_stats",
                    onClick = { onSelectTab(MainTab.STATS) }
                )
            )

            items(moduleCards.chunked(columns).size) { rowIndex ->
                val rowItems = moduleCards.chunked(columns)[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { data ->
                        ModuleGridCard(data = data, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Bulk Importer & PDF Export Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Import, Backup & Printable PDF Export", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Import shift PDF text or JSON backups, or generate printable A4 MCQ worksheets.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenImporter,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_import_card")
                            ) {
                                Text("Import / JSON")
                            }
                            FilledTonalButton(
                                onClick = onOpenPdfExport,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_export_pdf_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null
                                )
                                Text(" Export PDF")
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ModuleCardData(
    val badge: String,
    val countLabel: String,
    val title: String,
    val desc: String,
    val badgeColor: Color,
    val icon: ImageVector,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
private fun ModuleGridCard(data: ModuleCardData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = data.onClick)
            .testTag(data.tag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = data.badgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = data.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = data.badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = data.countLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(data.icon, contentDescription = null, tint = data.badgeColor)
                Text(data.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                text = data.desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
