package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AchievementBadgeDef
import com.example.data.model.DailyStudyGoalState
import com.example.data.model.GamificationLogic
import com.example.data.model.GamificationState
import com.example.data.model.WeeklyChallengeProgress
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.editorialColors
import java.util.Locale

@Composable
fun CompactGamificationBannerCard(
    gamification: GamificationState,
    studyGoal: DailyStudyGoalState,
    onOpenHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val levelInfo = remember(gamification.totalXp) {
        GamificationLogic.computeLevelProgress(gamification.totalXp)
    }
    val weeklyList = remember(gamification) {
        GamificationLogic.weeklyChallengeProgress(gamification)
    }
    val nextChallenge = remember(weeklyList) {
        weeklyList.firstOrNull { !it.claimed } ?: weeklyList.firstOrNull()
    }
    val activeStreak = studyGoal.effectiveCurrentStreak()
    val shieldReadyForRecovery = gamification.streakShields > 0 && studyGoal.canRecoverStreakToday()
    val xpRemaining = (levelInfo.xpForNextLevel - levelInfo.xpIntoLevel).coerceAtLeast(0)

    Card(
        onClick = onOpenHub,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(
            1.dp,
            if (shieldReadyForRecovery) editorial.acidLime else editorial.borderSubtle
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_gamification_banner")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = editorial.acidLime
                    ) {
                        Text(
                            text = "LV ${levelInfo.level}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                            color = editorial.onAcidLime,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = levelInfo.rankTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${gamification.unlockedBadgeIds.size}/${GamificationLogic.ALL_BADGES.size} badges · $xpRemaining XP to Lv ${levelInfo.level + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${levelInfo.totalXp} XP",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                        fontWeight = FontWeight.Bold
                    )
                    if (gamification.streakShields > 0 || activeStreak > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (activeStreak > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalFireDepartment,
                                        contentDescription = "Active streak",
                                        tint = editorial.correctText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${activeStreak}d",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        color = editorial.correctText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (gamification.streakShields > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = "Streak shields",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${gamification.streakShields}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AcidLimeProgressBar(
                progress = levelInfo.progressFraction,
                height = 6.dp
            )

            if (shieldReadyForRecovery) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = editorial.correctBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Streak Shield active",
                            tint = editorial.correctText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Streak Shield ready: hit today's goal to protect your ${studyGoal.currentStreakDays}-day streak.",
                            style = MaterialTheme.typography.labelSmall,
                            color = editorial.correctText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (nextChallenge != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (nextChallenge.claimed) {
                            "Weekly: ${nextChallenge.def.title} complete"
                        } else {
                            "Weekly: ${nextChallenge.def.title} (${nextChallenge.current}/${nextChallenge.def.target} · +${nextChallenge.def.rewardXp} XP)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Badges",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open gamification hub",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamificationHubCard(
    gamification: GamificationState,
    studyGoal: DailyStudyGoalState,
    modifier: Modifier = Modifier
) {
    val editorial = MaterialTheme.editorialColors
    val levelInfo = remember(gamification.totalXp) {
        GamificationLogic.computeLevelProgress(gamification.totalXp)
    }
    val weeklyChallenges = remember(gamification) {
        GamificationLogic.weeklyChallengeProgress(gamification)
    }
    val unlockedSet = remember(gamification.unlockedBadgeIds) {
        gamification.unlockedBadgeIds.toSet()
    }
    val shieldReadyForRecovery = gamification.streakShields > 0 && studyGoal.canRecoverStreakToday()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = editorial.cardSurface),
        border = BorderStroke(1.dp, editorial.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_gamification_hub_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Level, Nursing Rank & XP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "CAREER PROGRESSION & MILESTONES",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Level ${levelInfo.level} · ${levelInfo.rankTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Next rank: ${levelInfo.nextRankTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = editorial.acidLime
                ) {
                    Text(
                        text = "${levelInfo.totalXp} XP",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                        color = editorial.onAcidLime,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AcidLimeProgressBar(progress = levelInfo.progressFraction, height = 8.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${levelInfo.xpIntoLevel} / ${levelInfo.xpForNextLevel} XP to Level ${levelInfo.level + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${gamification.weeklyXp} XP this week",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = editorial.correctText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Fair Streak Recovery Shield Status
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = editorial.elevatedSurface,
                border = BorderStroke(
                    1.dp,
                    if (shieldReadyForRecovery) editorial.acidLime else editorial.borderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gamification_streak_shield_panel")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = "Streak Shield",
                        tint = if (gamification.streakShields > 0) editorial.correctText else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Streak Shield (${gamification.streakShields}/${GamificationLogic.MAX_STREAK_SHIELDS} banked)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (shieldReadyForRecovery) {
                                "Active today! Meet your daily goal to save your ${studyGoal.currentStreakDays}-day streak."
                            } else {
                                "Earn 1 free shield every 7-day streak. Automatically protects a single missed day."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = editorial.borderSubtle)

            // Weekly Challenges
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weekly Challenges",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = GamificationLogic.currentIsoWeekKey().uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                weeklyChallenges.forEach { ch ->
                    WeeklyChallengeRow(challenge = ch)
                }
            }

            HorizontalDivider(color = editorial.borderSubtle)

            // Achievement Badges
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Achievement Badges",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${unlockedSet.size} / ${GamificationLogic.ALL_BADGES.size} unlocked",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GamificationLogic.ALL_BADGES.forEach { badge ->
                        AchievementBadgeChip(
                            badge = badge,
                            unlocked = badge.id in unlockedSet
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyChallengeRow(challenge: WeeklyChallengeProgress) {
    val editorial = MaterialTheme.editorialColors
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_challenge_${challenge.def.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (challenge.claimed) Icons.Filled.CheckCircle else Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = if (challenge.claimed) editorial.correctText else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.def.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = challenge.def.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (challenge.claimed) editorial.correctBg else editorial.elevatedSurface
            ) {
                Text(
                    text = if (challenge.claimed) {
                        "CLAIMED"
                    } else {
                        "${challenge.current}/${challenge.def.target} · +${challenge.def.rewardXp} XP"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                    color = if (challenge.claimed) editorial.correctText else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        AcidLimeProgressBar(
            progress = challenge.progressFraction,
            height = 5.dp
        )
    }
}

@Composable
private fun AchievementBadgeChip(
    badge: AchievementBadgeDef,
    unlocked: Boolean
) {
    val editorial = MaterialTheme.editorialColors
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (unlocked) editorial.correctBg else editorial.elevatedSurface,
        border = BorderStroke(
            1.dp,
            if (unlocked) editorial.correctText.copy(alpha = 0.45f) else editorial.borderSubtle
        ),
        modifier = Modifier.testTag("badge_chip_${badge.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (unlocked) Icons.Filled.WorkspacePremium else Icons.Filled.MilitaryTech,
                contentDescription = if (unlocked) "${badge.title} unlocked" else "${badge.title} locked",
                tint = if (unlocked) editorial.correctText else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) editorial.correctText else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = badge.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
