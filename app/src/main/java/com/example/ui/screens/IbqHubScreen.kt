package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.IbqCategory
import com.example.data.model.IbqHubLogic
import com.example.data.model.IbqStatusFilter
import com.example.data.model.QuestionItem
import com.example.ui.components.ClinicalQuestionImageCard
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IbqHubScreen(
    allIbqQuestions: List<QuestionItem>,
    initialCategory: IbqCategory = IbqCategory.ALL,
    onBack: () -> Unit,
    onStartPractice: (List<QuestionItem>, String) -> Unit,
    onOpenEcgStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val editorial = MaterialTheme.editorialColors
    var selectedCategory by rememberSaveable(initialCategory) { mutableStateOf(initialCategory) }
    var statusFilter by rememberSaveable { mutableStateOf(IbqStatusFilter.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val summary = remember(allIbqQuestions) {
        IbqHubLogic.buildSummary(allIbqQuestions)
    }
    val filteredItems = remember(allIbqQuestions, selectedCategory, statusFilter, searchQuery) {
        IbqHubLogic.filterQuestions(
            items = allIbqQuestions,
            category = selectedCategory,
            statusFilter = statusFilter,
            searchQuery = searchQuery
        )
    }
    val smartOrderedAll = remember(allIbqQuestions) {
        IbqHubLogic.prioritizeForDrill(allIbqQuestions)
    }
    val smartOrderedFiltered = remember(filteredItems) {
        IbqHubLogic.prioritizeForDrill(filteredItems)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Image-Based Questions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${summary.totalCount} offline NORCET spotters · Pinch-to-zoom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("ibq_hub_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back from IBQ Hub"
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = editorial.acidLimeMuted,
                        border = BorderStroke(1.dp, editorial.acidLime.copy(alpha = 0.65f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "${summary.attemptedCount}/${summary.totalCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = if (editorial.isDark) editorial.acidLime else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 980.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Hero Mastery & Quick Launch Card
                item(key = "ibq_hero") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ibq_hub_hero_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "CLINICAL VISUAL SPOTTER BANK",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = editorial.acidLimeForeground
                                    )
                                    Text(
                                        text = "Visuals are auto-mixed into daily & 25Q drills",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = editorial.elevatedSurface,
                                    border = BorderStroke(1.dp, editorial.borderSubtle)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = if (summary.attemptedCount > 0) "${summary.accuracyPercent}%" else "NEW",
                                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Accuracy",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IbqMetricPill("Unseen", summary.unseenCount.toString(), Modifier.weight(1f))
                                IbqMetricPill("Mistakes", summary.mistakeCount.toString(), Modifier.weight(1f))
                                IbqMetricPill("Mastered", summary.correctCount.toString(), Modifier.weight(1f))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val set = smartOrderedAll
                                        if (set.isNotEmpty()) {
                                            onStartPractice(set, "Smart IBQ Spotters (${set.size}Q)")
                                        }
                                    },
                                    enabled = smartOrderedAll.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = editorial.acidLime,
                                        contentColor = editorial.onAcidLime
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("ibq_hub_start_smart_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Smart IBQ Drill",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (selectedCategory != IbqCategory.ALL || statusFilter != IbqStatusFilter.ALL || searchQuery.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            if (smartOrderedFiltered.isNotEmpty()) {
                                                onStartPractice(
                                                    smartOrderedFiltered,
                                                    "IBQ • ${selectedCategory.shortLabel} (${smartOrderedFiltered.size}Q)"
                                                )
                                            }
                                        },
                                        enabled = smartOrderedFiltered.isNotEmpty(),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, editorial.borderSubtle),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("ibq_hub_start_filtered_btn")
                                    ) {
                                        Text(
                                            text = "Drill Filtered (${smartOrderedFiltered.size})",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Category Filter Strip
                item(key = "ibq_categories") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            summary.categoryStats.forEach { stat ->
                                MonochromeFilterPill(
                                    label = "${stat.category.shortLabel} (${stat.totalCount})",
                                    selected = selectedCategory == stat.category,
                                    onClick = { selectedCategory = stat.category },
                                    modifier = Modifier.testTag("ibq_cat_${stat.category.name.lowercase(Locale.US)}")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IbqStatusFilter.entries.forEach { st ->
                                MonochromeFilterPill(
                                    label = st.label,
                                    selected = statusFilter == st,
                                    onClick = { statusFilter = st },
                                    modifier = Modifier.testTag("ibq_status_${st.name.lowercase(Locale.US)}")
                                )
                            }
                        }
                    }
                }

                // 3. Search Bar + Optional ECG Studio Shortcut
                item(key = "ibq_search_and_ecg") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search ECG, CTG, CXR, cannula, BMW, forceps...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear IBQ search"
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = editorial.borderSubtle,
                                focusedBorderColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ibq_hub_search_input")
                        )

                        if (selectedCategory == IbqCategory.ECG || selectedCategory == IbqCategory.ALL) {
                            Surface(
                                onClick = onOpenEcgStudio,
                                shape = RoundedCornerShape(14.dp),
                                color = editorial.elevatedSurface,
                                border = BorderStroke(1.dp, editorial.borderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ibq_hub_ecg_studio_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MonitorHeart,
                                            contentDescription = null,
                                            tint = editorial.acidLimeForeground,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Need systematic 9-point ECG mastery?",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Open Dedicated ECG Learning Studio (14 ICU strips)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "OPEN →",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = editorial.acidLimeForeground
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Empty State or Visual Gallery Cards
                if (filteredItems.isEmpty()) {
                    item(key = "ibq_empty") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "No IBQs match these filters",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Try switching back to All IBQs or clearing your status/search filter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = {
                                        selectedCategory = IbqCategory.ALL
                                        statusFilter = IbqStatusFilter.ALL
                                        searchQuery = ""
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        IbqGalleryCard(
                            item = item,
                            onPracticeSingle = {
                                // Start with this card first, followed by remaining items in current category
                                val ordered = listOf(item) + filteredItems.filterNot { it.id == item.id }
                                onStartPractice(
                                    ordered,
                                    "IBQ • ${IbqHubLogic.shortSpotterTitle(item.question)}"
                                )
                            }
                        )
                    }
                }

                item(key = "ibq_bottom_spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun IbqMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = editorial.elevatedSurface,
        border = BorderStroke(1.dp, editorial.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IbqGalleryCard(
    item: QuestionItem,
    onPracticeSingle: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val q = item.question
    val category = remember(q.id, q.imageUrl) { IbqHubLogic.categorize(q) }
    val title = remember(q.id, q.imageUrl) { IbqHubLogic.shortSpotterTitle(q) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ibq_gallery_card_${q.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row: Category, Subject, Difficulty & Attempt status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = editorial.elevatedSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle)
                    ) {
                        Text(
                            text = category.shortLabel.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = editorial.elevatedSurface
                    ) {
                        Text(
                            text = "${q.subject} · ${q.difficulty.label}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                when {
                    item.lastOutcome == "right" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Mastered",
                                tint = editorial.correctText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Mastered",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = editorial.correctText
                            )
                        }
                    }
                    item.lastOutcome == "wrong" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = "Needs review",
                                tint = editorial.wrongText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Review",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = editorial.wrongText
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "UNSEEN",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = editorial.acidLimeForeground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Zoomable visual preview
            ClinicalQuestionImageCard(
                imageUrl = q.imageUrl,
                viewportHeight = 176.dp
            )

            // Title & truncated clinical vignette
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = q.questionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onPracticeSingle,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("ibq_practice_btn_${q.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (item.attemptsCount == 0) "Solve Visual Question" else "Practice Again",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
