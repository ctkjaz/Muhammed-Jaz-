package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Flashcard
import com.example.data.model.RapidRecallCard
import com.example.data.model.RapidRecallCatalog
import com.example.data.model.RapidRecallDeckCategory
import com.example.ui.components.AcidLimeProgressBar
import com.example.ui.components.MonochromeFilterPill
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors

@Composable
fun FlashcardDeckScreen(
    caFlashcards: List<Flashcard>,
    masteredIds: Set<String>,
    onMarkCardMastered: (String, Boolean) -> Unit,
    onResetMastery: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val editorial = MaterialTheme.editorialColors

    val unifiedDeck = remember(caFlashcards) { RapidRecallCatalog.buildUnifiedDeck(caFlashcards) }
    var selectedCategory by remember { mutableStateOf<RapidRecallDeckCategory?>(null) }
    var unmasteredOnly by remember { mutableStateOf(false) }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val activeDeck = remember(unifiedDeck, selectedCategory, masteredIds, unmasteredOnly, shuffleSeed) {
        val filtered = RapidRecallCatalog.filterDeck(
            deck = unifiedDeck,
            category = selectedCategory,
            masteredIds = masteredIds,
            unmasteredOnly = unmasteredOnly
        )
        if (shuffleSeed == 0) filtered else filtered.shuffled(kotlin.random.Random(shuffleSeed))
    }

    LaunchedEffect(activeDeck.size, selectedCategory, unmasteredOnly, shuffleSeed) {
        if (currentIndex >= activeDeck.size) {
            currentIndex = 0
        }
        isFlipped = false
    }

    val totalMastered = remember(unifiedDeck, masteredIds) {
        unifiedDeck.count { it.id in masteredIds }
    }
    val masteryRatio = if (unifiedDeck.isNotEmpty()) totalMastered.toFloat() / unifiedDeck.size.toFloat() else 0f

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("flashcard_studio_back_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Rapid Recall Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$totalMastered / ${unifiedDeck.size} cards mastered · Tap card to flip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    shuffleSeed++
                    currentIndex = 0
                    isFlipped = false
                },
                modifier = Modifier.testTag("flashcard_shuffle_btn")
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle deck")
            }
        }
        HorizontalDivider(color = editorial.borderSubtle)

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mastery progress bar + Reset
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DECK MASTERY ${(masteryRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (totalMastered > 0) {
                            TextButton(
                                onClick = onResetMastery,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.testTag("flashcard_reset_mastery_btn")
                            ) {
                                Text("Reset Progress", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    AcidLimeProgressBar(progress = masteryRatio, modifier = Modifier.fillMaxWidth())
                }

                // Category filter pills + Unmastered Only toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MonochromeFilterPill(
                        label = if (unmasteredOnly) "Unmastered Only ✓" else "Unmastered Only",
                        selected = unmasteredOnly,
                        onClick = {
                            unmasteredOnly = !unmasteredOnly
                            currentIndex = 0
                            isFlipped = false
                        },
                        modifier = Modifier.testTag("flashcard_unmastered_filter")
                    )
                    MonochromeFilterPill(
                        label = "All (${unifiedDeck.size})",
                        selected = selectedCategory == null,
                        onClick = {
                            selectedCategory = null
                            currentIndex = 0
                            isFlipped = false
                        }
                    )
                    RapidRecallDeckCategory.entries.forEach { cat ->
                        val count = unifiedDeck.count { it.category == cat }
                        if (count > 0) {
                            MonochromeFilterPill(
                                label = "${cat.label} ($count)",
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = cat
                                    currentIndex = 0
                                    isFlipped = false
                                }
                            )
                        }
                    }
                }

                if (activeDeck.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
                        border = BorderStroke(1.dp, editorial.borderSubtle),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = editorial.correctText,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "All Cards Mastered!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "You have mastered every card in this filter. Switch off 'Unmastered Only' or pick another category to keep revising.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (unmasteredOnly) {
                                Button(
                                    onClick = { unmasteredOnly = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = editorial.acidLime,
                                        contentColor = editorial.onAcidLime
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Show Full Deck", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    val card = activeDeck[currentIndex.coerceIn(0, activeDeck.lastIndex)]
                    val isCardMastered = card.id in masteredIds

                    FlipFlashcardStage(
                        card = card,
                        isFlipped = isFlipped,
                        isMastered = isCardMastered,
                        positionLabel = "${currentIndex + 1} / ${activeDeck.size}",
                        onToggleFlip = { isFlipped = !isFlipped }
                    )

                    // Mastery Actions (Got It / Review Again)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onMarkCardMastered(card.id, false)
                                isFlipped = false
                                if (activeDeck.size > 1) {
                                    currentIndex = (currentIndex + 1) % activeDeck.size
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .testTag("flashcard_review_again_btn")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Review Again", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                onMarkCardMastered(card.id, true)
                                isFlipped = false
                                if (!unmasteredOnly && activeDeck.size > 1) {
                                    currentIndex = (currentIndex + 1) % activeDeck.size
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = editorial.acidLime,
                                contentColor = editorial.onAcidLime
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .testTag("flashcard_got_it_btn")
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(if (isCardMastered) "Mastered ✓" else "Got It!", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Previous / Next Card Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                isFlipped = false
                                currentIndex = if (currentIndex > 0) currentIndex - 1 else activeDeck.lastIndex
                            },
                            enabled = activeDeck.size > 1,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("flashcard_prev_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Prev")
                        }
                        Text(
                            text = "Card ${currentIndex + 1} of ${activeDeck.size}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = {
                                isFlipped = false
                                currentIndex = (currentIndex + 1) % activeDeck.size
                            },
                            enabled = activeDeck.size > 1,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, editorial.borderSubtle),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("flashcard_next_btn")
                        ) {
                            Text("Next")
                            Spacer(Modifier.size(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipFlashcardStage(
    card: RapidRecallCard,
    isFlipped: Boolean,
    isMastered: Boolean,
    positionLabel: String,
    onToggleFlip: () -> Unit
) {
    val editorial = MaterialTheme.editorialColors
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "flashcard_flip_rotation"
    )
    val showingBack = rotation > 90f

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(
            width = if (showingBack) 1.5.dp else 1.dp,
            color = if (showingBack) editorial.acidLime else editorial.borderSubtle
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 290.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable(onClick = onToggleFlip)
            .testTag("flashcard_flip_surface")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 290.dp)
                .graphicsLayer {
                    if (showingBack) rotationY = 180f
                }
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (showingBack) editorial.acidLime else editorial.elevatedSurface
                    ) {
                        Text(
                            text = if (showingBack) "ANSWER" else card.category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = if (showingBack) editorial.onAcidLime else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (isMastered) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = editorial.correctBg
                        ) {
                            Text(
                                text = "MASTERED",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                fontWeight = FontWeight.Bold,
                                color = editorial.correctText,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = positionLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))

            if (!showingBack) {
                // Front face
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = card.frontPrompt,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            lineHeight = 29.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = card.examTag,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Back face
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = card.backAnswer,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = editorial.elevatedSurface,
                        border = BorderStroke(1.dp, editorial.borderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = card.examPearl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Flip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = if (showingBack) "Tap card to see prompt again" else "Tap card to reveal clinical answer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
