package com.example.data.model

import java.util.Calendar
import java.util.Locale

data class GamificationState(
    val totalXp: Int = 0,
    val weeklyXp: Int = 0,
    val weeklyKey: String = "",
    val weeklyQuestions: Int = 0,
    val weeklyCorrect: Int = 0,
    val weeklySessions: Int = 0,
    val weeklyClaimedIds: List<String> = emptyList(),
    val unlockedBadgeIds: List<String> = emptyList(),
    val streakShields: Int = 0,
    val lastShieldEarnedStreak: Int = 0,
    val lastStreakRecoveryDate: String = "",
    val qotdAnsweredCount: Int = 0,
    val completedSessionsCount: Int = 0,
    val focusSprintsCount: Int = 0,
    val srsMasteredCount: Int = 0,
    val bestSessionAccuracy: Int = 0,
    // Local-only anti-farming & deduplication state (not required in Firestore)
    val dailyAwardDateKey: String = "",
    val dailyAwardedQuestionIds: List<String> = emptyList(),
    val lastDailyGoalBonusDate: String = ""
)

data class LevelProgressInfo(
    val level: Int,
    val rankTitle: String,
    val nextRankTitle: String,
    val totalXp: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int,
    val progressFraction: Float
)

data class AchievementBadgeDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String
)

data class WeeklyChallengeDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val target: Int,
    val rewardXp: Int
)

data class WeeklyChallengeProgress(
    val def: WeeklyChallengeDef,
    val current: Int,
    val claimed: Boolean
) {
    val progressFraction: Float
        get() = (current.toFloat() / def.target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val isComplete: Boolean
        get() = current >= def.target || claimed
}

data class GamificationUpdateResult(
    val updatedState: GamificationState,
    val xpAwarded: Int = 0,
    val leveledUpTo: Int? = null,
    val newlyUnlockedBadges: List<AchievementBadgeDef> = emptyList(),
    val newlyClaimedChallenges: List<WeeklyChallengeDef> = emptyList(),
    val earnedStreakShield: Boolean = false
)

object GamificationLogic {
    const val XP_CORRECT_ANSWER = 10
    const val XP_INCORRECT_ATTEMPT = 2
    const val XP_SESSION_20Q_BONUS = 25
    const val XP_DAILY_GOAL_BONUS = 50
    const val XP_STREAK_7DAY_BONUS = 75
    const val XP_QOTD_CORRECT = 15
    const val XP_QOTD_ATTEMPT = 5
    const val XP_FOCUS_SPRINT = 15
    const val XP_SRS_MASTERED = 10

    const val SESSION_BONUS_MIN_QUESTIONS = 20
    const val MAX_STREAK_SHIELDS = 2
    const val MAX_TOTAL_XP = 10_000_000
    const val MAX_WEEKLY_XP = 100_000
    const val MAX_WEEKLY_QUESTIONS = 10_000
    const val MAX_WEEKLY_SESSIONS = 1_000
    const val MAX_COUNTER_BOUND = 100_000
    const val MAX_BADGES_COUNT = 32
    const val MAX_WEEKLY_CLAIMED_COUNT = 10
    const val MAX_DAILY_DEDUP_IDS = 250
    const val MAX_LEVEL = 50

    const val BADGE_FIRST_STEPS = "first_steps"
    const val BADGE_CENTURION = "centurion"
    const val BADGE_CLINICAL_SCHOLAR = "clinical_scholar"
    const val BADGE_STREAK_7 = "streak_7"
    const val BADGE_STREAK_30 = "streak_30"
    const val BADGE_MOCK_FINISHER = "mock_finisher"
    const val BADGE_SHARPSHOOTER = "sharpshooter"
    const val BADGE_QOTD_5 = "qotd_5"
    const val BADGE_SRS_MASTER_5 = "srs_master_5"
    const val BADGE_DEEP_FOCUS_3 = "deep_focus_3"

    const val CHALLENGE_WEEKLY_100Q = "weekly_100q"
    const val CHALLENGE_WEEKLY_50_CORRECT = "weekly_50_correct"
    const val CHALLENGE_WEEKLY_3_SESSIONS = "weekly_3_sessions"

    val ALL_BADGES: List<AchievementBadgeDef> = listOf(
        AchievementBadgeDef(BADGE_FIRST_STEPS, "First Steps", "Solve 10 nursing questions", "Practice"),
        AchievementBadgeDef(BADGE_CENTURION, "Centurion", "Solve 100 questions", "Practice"),
        AchievementBadgeDef(BADGE_CLINICAL_SCHOLAR, "Clinical Scholar", "Solve 500 questions", "Mastery"),
        AchievementBadgeDef(BADGE_STREAK_7, "7-Day Rhythm", "Hit a 7-day study streak", "Consistency"),
        AchievementBadgeDef(BADGE_STREAK_30, "30-Day Discipline", "Hit a 30-day study streak", "Consistency"),
        AchievementBadgeDef(BADGE_MOCK_FINISHER, "Exam Finisher", "Complete a 20+ Q session or mock", "Exams"),
        AchievementBadgeDef(BADGE_SHARPSHOOTER, "Sharpshooter", "Score 80%+ on a 20+ Q test", "Accuracy"),
        AchievementBadgeDef(BADGE_QOTD_5, "Daily Thinker", "Complete 5 Questions of the Day", "Daily"),
        AchievementBadgeDef(BADGE_SRS_MASTER_5, "Mistake Slayer", "Master 5 SRS Notebook cards", "Revision"),
        AchievementBadgeDef(BADGE_DEEP_FOCUS_3, "Deep Focus", "Finish 3 timed study sprints", "Focus")
    )

    val VALID_BADGE_IDS: Set<String> = ALL_BADGES.map { it.id }.toSet()

    val WEEKLY_CHALLENGES: List<WeeklyChallengeDef> = listOf(
        WeeklyChallengeDef(
            id = CHALLENGE_WEEKLY_100Q,
            title = "Weekly Volume",
            subtitle = "Solve 100 questions this week",
            target = 100,
            rewardXp = 60
        ),
        WeeklyChallengeDef(
            id = CHALLENGE_WEEKLY_50_CORRECT,
            title = "Clinical Precision",
            subtitle = "Get 50 accurate answers this week",
            target = 50,
            rewardXp = 50
        ),
        WeeklyChallengeDef(
            id = CHALLENGE_WEEKLY_3_SESSIONS,
            title = "Exam Stamina",
            subtitle = "Finish 3 full sessions or focus sprints",
            target = 3,
            rewardXp = 40
        )
    )

    val VALID_CHALLENGE_IDS: Set<String> = WEEKLY_CHALLENGES.map { it.id }.toSet()

    fun currentIsoWeekKey(nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(Locale.US)
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.minimalDaysInFirstWeek = 4
        cal.timeInMillis = nowMs
        val weekYear = if (cal.isWeekDateSupported) cal.weekYear else cal.get(Calendar.YEAR)
        val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", weekYear, weekOfYear)
    }

    fun sanitizeWeekKey(raw: String): String {
        val clean = raw.trim().take(16)
        return if (clean.matches(Regex("^\\d{4}-W\\d{2}$"))) clean else ""
    }

    fun xpRequiredToAdvanceFromLevel(level: Int): Int {
        val safeLevel = level.coerceIn(1, MAX_LEVEL)
        return 150 + (safeLevel - 1) * 50
    }

    fun rankTitleForLevel(level: Int): String = when {
        level <= 2 -> "Nursing Intern"
        level <= 5 -> "Ward Staff Nurse"
        level <= 9 -> "Clinical Nursing Officer"
        level <= 14 -> "Senior Nursing Officer"
        level <= 19 -> "ICU Clinical Specialist"
        level <= 29 -> "NORCET Top Ranker"
        else -> "Chief Nursing Mentor"
    }

    fun nextRankTitleForLevel(level: Int): String = when {
        level <= 2 -> "Ward Staff Nurse (Lv 3)"
        level <= 5 -> "Clinical Nursing Officer (Lv 6)"
        level <= 9 -> "Senior Nursing Officer (Lv 10)"
        level <= 14 -> "ICU Clinical Specialist (Lv 15)"
        level <= 19 -> "NORCET Top Ranker (Lv 20)"
        level <= 29 -> "Chief Nursing Mentor (Lv 30)"
        else -> "Peak Rank Achieved"
    }

    fun computeLevelProgress(totalXp: Int): LevelProgressInfo {
        val safeXp = totalXp.coerceIn(0, MAX_TOTAL_XP)
        var level = 1
        var remaining = safeXp
        while (level < MAX_LEVEL) {
            val need = xpRequiredToAdvanceFromLevel(level)
            if (remaining < need) break
            remaining -= need
            level++
        }
        val xpForNext = xpRequiredToAdvanceFromLevel(level)
        val fraction = if (level >= MAX_LEVEL) 1f else (remaining.toFloat() / xpForNext.toFloat()).coerceIn(0f, 1f)
        return LevelProgressInfo(
            level = level,
            rankTitle = rankTitleForLevel(level),
            nextRankTitle = nextRankTitleForLevel(level),
            totalXp = safeXp,
            xpIntoLevel = if (level >= MAX_LEVEL) xpForNext else remaining,
            xpForNextLevel = xpForNext,
            progressFraction = fraction
        )
    }

    fun normalizeForWeek(
        state: GamificationState,
        weekKey: String = currentIsoWeekKey()
    ): GamificationState {
        val cleanWeek = sanitizeWeekKey(weekKey).ifBlank { currentIsoWeekKey() }
        return if (state.weeklyKey == cleanWeek) {
            state
        } else {
            state.copy(
                weeklyKey = cleanWeek,
                weeklyXp = 0,
                weeklyQuestions = 0,
                weeklyCorrect = 0,
                weeklySessions = 0,
                weeklyClaimedIds = emptyList()
            )
        }
    }

    fun weeklyChallengeProgress(
        state: GamificationState,
        weekKey: String = currentIsoWeekKey()
    ): List<WeeklyChallengeProgress> {
        val normalized = normalizeForWeek(state, weekKey)
        val claimedSet = normalized.weeklyClaimedIds.toSet()
        return WEEKLY_CHALLENGES.map { def ->
            val currentVal = when (def.id) {
                CHALLENGE_WEEKLY_100Q -> normalized.weeklyQuestions
                CHALLENGE_WEEKLY_50_CORRECT -> normalized.weeklyCorrect
                CHALLENGE_WEEKLY_3_SESSIONS -> normalized.weeklySessions
                else -> 0
            }
            WeeklyChallengeProgress(
                def = def,
                current = currentVal.coerceAtMost(def.target),
                claimed = def.id in claimedSet
            )
        }
    }

    fun recordQuestionAnswer(
        current: GamificationState,
        questionId: String,
        isCorrect: Boolean,
        lifetimeAnsweredCount: Int,
        longestStreakDays: Int,
        todayKey: String = StudyGoalLogic.todayDateKey(),
        weekKey: String = currentIsoWeekKey()
    ): GamificationUpdateResult {
        val cleanQId = questionId.trim().take(64)
        val base = normalizeForWeek(current, weekKey)
        val sameDay = base.dailyAwardDateKey == todayKey
        val awardedToday = if (sameDay) base.dailyAwardedQuestionIds else emptyList()

        // Anti-farming guard: do not re-award XP for answering the exact same question twice today
        if (cleanQId.isNotEmpty() && cleanQId in awardedToday) {
            return GamificationUpdateResult(updatedState = base)
        }

        val xpDelta = if (isCorrect) XP_CORRECT_ANSWER else XP_INCORRECT_ATTEMPT
        val nextDedupIds = if (cleanQId.isNotEmpty()) {
            (awardedToday + cleanQId).takeLast(MAX_DAILY_DEDUP_IDS)
        } else {
            awardedToday
        }

        val progressed = base.copy(
            totalXp = (base.totalXp + xpDelta).coerceIn(0, MAX_TOTAL_XP),
            weeklyXp = (base.weeklyXp + xpDelta).coerceIn(0, MAX_WEEKLY_XP),
            weeklyQuestions = (base.weeklyQuestions + 1).coerceIn(0, MAX_WEEKLY_QUESTIONS),
            weeklyCorrect = (base.weeklyCorrect + if (isCorrect) 1 else 0).coerceIn(0, MAX_WEEKLY_QUESTIONS),
            dailyAwardDateKey = todayKey,
            dailyAwardedQuestionIds = nextDedupIds
        )
        return finalizeWithChallengesAndBadges(
            previous = base,
            candidate = progressed,
            baseXpAwarded = xpDelta,
            lifetimeAnsweredCount = lifetimeAnsweredCount,
            longestStreakDays = longestStreakDays
        )
    }

    fun recordBatchSessionCompleted(
        current: GamificationState,
        results: List<Pair<String, Boolean>>,
        accuracyPercent: Int,
        lifetimeAnsweredCount: Int,
        longestStreakDays: Int,
        todayKey: String = StudyGoalLogic.todayDateKey(),
        weekKey: String = currentIsoWeekKey()
    ): GamificationUpdateResult {
        val base = normalizeForWeek(current, weekKey)
        val sameDay = base.dailyAwardDateKey == todayKey
        val alreadyAwarded = (if (sameDay) base.dailyAwardedQuestionIds else emptyList()).toMutableList()
        val seenSet = alreadyAwarded.toMutableSet()

        var answerXp = 0
        var newQuestions = 0
        var newCorrect = 0
        results.forEach { (rawId, isRight) ->
            val cleanId = rawId.trim().take(64)
            if (cleanId.isEmpty() || seenSet.add(cleanId)) {
                answerXp += if (isRight) XP_CORRECT_ANSWER else XP_INCORRECT_ATTEMPT
                newQuestions++
                if (isRight) newCorrect++
                if (cleanId.isNotEmpty()) alreadyAwarded.add(cleanId)
            }
        }

        val qualifiesForSessionBonus = results.size >= SESSION_BONUS_MIN_QUESTIONS
        val sessionBonus = if (qualifiesForSessionBonus) XP_SESSION_20Q_BONUS else 0
        val totalBaseXp = answerXp + sessionBonus
        val safeAcc = accuracyPercent.coerceIn(0, 100)
        val nextBestAcc = if (qualifiesForSessionBonus) maxOf(base.bestSessionAccuracy, safeAcc) else base.bestSessionAccuracy

        val progressed = base.copy(
            totalXp = (base.totalXp + totalBaseXp).coerceIn(0, MAX_TOTAL_XP),
            weeklyXp = (base.weeklyXp + totalBaseXp).coerceIn(0, MAX_WEEKLY_XP),
            weeklyQuestions = (base.weeklyQuestions + newQuestions).coerceIn(0, MAX_WEEKLY_QUESTIONS),
            weeklyCorrect = (base.weeklyCorrect + newCorrect).coerceIn(0, MAX_WEEKLY_QUESTIONS),
            weeklySessions = (base.weeklySessions + if (qualifiesForSessionBonus) 1 else 0).coerceIn(0, MAX_WEEKLY_SESSIONS),
            completedSessionsCount = (base.completedSessionsCount + if (qualifiesForSessionBonus) 1 else 0).coerceIn(0, MAX_COUNTER_BOUND),
            bestSessionAccuracy = nextBestAcc,
            dailyAwardDateKey = todayKey,
            dailyAwardedQuestionIds = alreadyAwarded.takeLast(MAX_DAILY_DEDUP_IDS)
        )
        return finalizeWithChallengesAndBadges(
            previous = base,
            candidate = progressed,
            baseXpAwarded = totalBaseXp,
            lifetimeAnsweredCount = lifetimeAnsweredCount,
            longestStreakDays = longestStreakDays
        )
    }

    fun recordTimedPracticeCompleted(
        current: GamificationState,
        answeredCount: Int,
        accuracyPercent: Int,
        lifetimeAnsweredCount: Int,
        longestStreakDays: Int,
        weekKey: String = currentIsoWeekKey()
    ): GamificationUpdateResult {
        val base = normalizeForWeek(current, weekKey)
        if (answeredCount < SESSION_BONUS_MIN_QUESTIONS) {
            return GamificationUpdateResult(updatedState = base)
        }
        val safeAcc = accuracyPercent.coerceIn(0, 100)
        val progressed = base.copy(
            totalXp = (base.totalXp + XP_SESSION_20Q_BONUS).coerceIn(0, MAX_TOTAL_XP),
            weeklyXp = (base.weeklyXp + XP_SESSION_20Q_BONUS).coerceIn(0, MAX_WEEKLY_XP),
            weeklySessions = (base.weeklySessions + 1).coerceIn(0, MAX_WEEKLY_SESSIONS),
            completedSessionsCount = (base.completedSessionsCount + 1).coerceIn(0, MAX_COUNTER_BOUND),
            bestSessionAccuracy = maxOf(base.bestSessionAccuracy, safeAcc)
        )
        return finalizeWithChallengesAndBadges(
            previous = base,
            candidate = progressed,
            baseXpAwarded = XP_SESSION_20Q_BONUS,
            lifetimeAnsweredCount = lifetimeAnsweredCount,
            longestStreakDays = longestStreakDays
        )
    }

    fun recordQotdCompleted(
        current: GamificationState,
        isCorrect: Boolean,
        lifetimeAnsweredCount: Int,
        longestStreakDays: Int,
        weekKey: String = currentIsoWeekKey()
    ): GamificationUpdateResult {
        val base = normalizeForWeek(current, weekKey)
        val xpDelta = if (isCorrect) XP_QOTD_CORRECT else XP_QOTD_ATTEMPT
        val progressed = base.copy(
            totalXp = (base.totalXp + xpDelta).coerceIn(0, MAX_TOTAL_XP),
            weeklyXp = (base.weeklyXp + xpDelta).coerceIn(0, MAX_WEEKLY_XP),
            weeklyQuestions = (base.weeklyQuestions + 1).coerceIn(0, MAX_WEEKLY_QUESTIONS),
            weeklyCorrect = (base.weeklyCorrect + if (isCorrect) 1 else 0).coerceIn(0, MAX_WEEKLY_QUESTIONS),
            qotdAnsweredCount = (base.qotdAnsweredCount + 1).coerceIn(0, MAX_COUNTER_BOUND)
        )
        return finalizeWithChallengesAndBadges(
            previous = base,
            candidate = progressed,
            baseXpAwarded = xpDelta,
            lifetimeAnsweredCount = lifetimeAnsweredCount,
            longestStreakDays = longestStreakDays
        )
    }

    fun recordDailyGoalAndStreakEvent(
        current: GamificationState,
        justMetDailyGoal: Boolean,
        usedStreakRecovery: Boolean,
        currentStreakDays: Int,
        longestStreakDays: Int,
        lifetimeAnsweredCount: Int,
        todayKey: String = StudyGoalLogic.todayDateKey(),
        weekKey: String = currentIsoWeekKey()
    ): GamificationUpdateResult {
        val base = normalizeForWeek(current, weekKey)
        var xpDelta = 0
        var nextShields = base.streakShields.coerceIn(0, MAX_STREAK_SHIELDS)
        var nextRecoveryDate = base.lastStreakRecoveryDate
        var nextLastShieldMilestone = base.lastShieldEarnedStreak
        var earnedShield = false
        var nextGoalBonusDate = base.lastDailyGoalBonusDate

        if (usedStreakRecovery && nextShields > 0) {
            nextShields = (nextShields - 1).coerceAtLeast(0)
            nextRecoveryDate = StudyGoalLogic.sanitizeDateKey(todayKey)
        }

        if (justMetDailyGoal && base.lastDailyGoalBonusDate != todayKey) {
            xpDelta += XP_DAILY_GOAL_BONUS
            nextGoalBonusDate = StudyGoalLogic.sanitizeDateKey(todayKey)
        }

        // Fair 7-day streak milestone: award +75 XP and 1 free Streak Shield (max 2 banked)
        val milestoneReached = (currentStreakDays / 7) * 7
        if (milestoneReached >= 7 && milestoneReached > base.lastShieldEarnedStreak) {
            xpDelta += XP_STREAK_7DAY_BONUS
            nextLastShieldMilestone = milestoneReached
            if (nextShields < MAX_STREAK_SHIELDS) {
                nextShields++
                earnedShield = true
            }
        }

        val progressed = base.copy(
            totalXp = (base.totalXp + xpDelta).coerceIn(0, MAX_TOTAL_XP),
            weeklyXp = (base.weeklyXp + xpDelta).coerceIn(0, MAX_WEEKLY_XP),
            streakShields = nextShields,
            lastShieldEarnedStreak = nextLastShieldMilestone,
            lastStreakRecoveryDate = nextRecoveryDate,
            lastDailyGoalBonusDate = nextGoalBonusDate
        )
        return finalizeWithChallengesAndBadges(
            previous = base,
            candidate = progressed,
            baseXpAwarded = xpDelta,
            lifetimeAnsweredCount = lifetimeAnsweredCount,
            longestStreakDays = maxOf(longestStreakDays, currentStreakDays),
            earnedShield = earnedShield
        )
    }

    fun recordFocusSprintCompleted(
        current: GamificationState,
        lifetimeAnsweredCount: Int,
        longestStreakDays: Int,
        weekKey: String = currentIsoWeekKey()
    ): GamificationUpdateResult {
        val base = normalizeForWeek(current, weekKey)
        val progressed = base.copy(
            totalXp = (base.totalXp + XP_FOCUS_SPRINT).coerceIn(0, MAX_TOTAL_XP),
            weeklyXp = (base.weeklyXp + XP_FOCUS_SPRINT).coerceIn(0, MAX_WEEKLY_XP),
            weeklySessions = (base.weeklySessions + 1).coerceIn(0, MAX_WEEKLY_SESSIONS),
            focusSprintsCount = (base.focusSprintsCount + 1).coerceIn(0, MAX_COUNTER_BOUND)
        )
        return finalizeWithChallengesAndBadges(
            previous = base,
            candidate = progressed,
            baseXpAwarded = XP_FOCUS_SPRINT,
            lifetimeAnsweredCount = lifetimeAnsweredCount,
            longestStreakDays = longestStreakDays
        )
    }

    fun recordSrsCardMastered(
        current: GamificationState,
        lifetimeAnsweredCount: Int,
        longestStreakDays: Int,
        weekKey: String = currentIsoWeekKey()
    ): GamificationUpdateResult {
        val base = normalizeForWeek(current, weekKey)
        val progressed = base.copy(
            totalXp = (base.totalXp + XP_SRS_MASTERED).coerceIn(0, MAX_TOTAL_XP),
            weeklyXp = (base.weeklyXp + XP_SRS_MASTERED).coerceIn(0, MAX_WEEKLY_XP),
            srsMasteredCount = (base.srsMasteredCount + 1).coerceIn(0, MAX_COUNTER_BOUND)
        )
        return finalizeWithChallengesAndBadges(
            previous = base,
            candidate = progressed,
            baseXpAwarded = XP_SRS_MASTERED,
            lifetimeAnsweredCount = lifetimeAnsweredCount,
            longestStreakDays = longestStreakDays
        )
    }

    private fun finalizeWithChallengesAndBadges(
        previous: GamificationState,
        candidate: GamificationState,
        baseXpAwarded: Int,
        lifetimeAnsweredCount: Int,
        longestStreakDays: Int,
        earnedShield: Boolean = false
    ): GamificationUpdateResult {
        // 1. Check and claim newly completed weekly challenges
        val claimedSet = candidate.weeklyClaimedIds.filter { it in VALID_CHALLENGE_IDS }.toMutableSet()
        val newlyClaimed = mutableListOf<WeeklyChallengeDef>()
        var challengeBonusXp = 0

        WEEKLY_CHALLENGES.forEach { def ->
            if (def.id !in claimedSet) {
                val value = when (def.id) {
                    CHALLENGE_WEEKLY_100Q -> candidate.weeklyQuestions
                    CHALLENGE_WEEKLY_50_CORRECT -> candidate.weeklyCorrect
                    CHALLENGE_WEEKLY_3_SESSIONS -> candidate.weeklySessions
                    else -> 0
                }
                if (value >= def.target) {
                    claimedSet.add(def.id)
                    newlyClaimed.add(def)
                    challengeBonusXp += def.rewardXp
                }
            }
        }

        // 2. Evaluate newly unlocked achievement badges
        val existingBadges = candidate.unlockedBadgeIds.filter { it in VALID_BADGE_IDS }.toMutableSet()
        val newlyUnlocked = mutableListOf<AchievementBadgeDef>()

        fun unlockIf(condition: Boolean, badgeId: String) {
            if (condition && existingBadges.add(badgeId)) {
                ALL_BADGES.firstOrNull { it.id == badgeId }?.let { newlyUnlocked.add(it) }
            }
        }

        unlockIf(lifetimeAnsweredCount >= 10, BADGE_FIRST_STEPS)
        unlockIf(lifetimeAnsweredCount >= 100, BADGE_CENTURION)
        unlockIf(lifetimeAnsweredCount >= 500, BADGE_CLINICAL_SCHOLAR)
        unlockIf(longestStreakDays >= 7, BADGE_STREAK_7)
        unlockIf(longestStreakDays >= 30, BADGE_STREAK_30)
        unlockIf(candidate.completedSessionsCount >= 1, BADGE_MOCK_FINISHER)
        unlockIf(candidate.bestSessionAccuracy >= 80 && candidate.completedSessionsCount >= 1, BADGE_SHARPSHOOTER)
        unlockIf(candidate.qotdAnsweredCount >= 5, BADGE_QOTD_5)
        unlockIf(candidate.srsMasteredCount >= 5, BADGE_SRS_MASTER_5)
        unlockIf(candidate.focusSprintsCount >= 3, BADGE_DEEP_FOCUS_3)

        val finalState = candidate.copy(
            totalXp = (candidate.totalXp + challengeBonusXp).coerceIn(0, MAX_TOTAL_XP),
            weeklyXp = (candidate.weeklyXp + challengeBonusXp).coerceIn(0, MAX_WEEKLY_XP),
            weeklyClaimedIds = claimedSet.toList().sorted().take(MAX_WEEKLY_CLAIMED_COUNT),
            unlockedBadgeIds = existingBadges.toList().sorted().take(MAX_BADGES_COUNT)
        )

        val prevLevel = computeLevelProgress(previous.totalXp).level
        val newLevel = computeLevelProgress(finalState.totalXp).level

        return GamificationUpdateResult(
            updatedState = finalState,
            xpAwarded = baseXpAwarded + challengeBonusXp,
            leveledUpTo = if (newLevel > prevLevel) newLevel else null,
            newlyUnlockedBadges = newlyUnlocked,
            newlyClaimedChallenges = newlyClaimed,
            earnedStreakShield = earnedShield
        )
    }

    fun mergeWithCloud(
        local: GamificationState,
        remote: GamificationState,
        currentWeekKey: String = currentIsoWeekKey()
    ): GamificationState {
        val localWeek = sanitizeWeekKey(local.weeklyKey)
        val remoteWeek = sanitizeWeekKey(remote.weeklyKey)

        val mergedWeekKey: String
        val mergedWeeklyXp: Int
        val mergedWeeklyQ: Int
        val mergedWeeklyCorrect: Int
        val mergedWeeklySessions: Int
        val mergedClaimed: List<String>

        when {
            localWeek == currentWeekKey && remoteWeek == currentWeekKey -> {
                mergedWeekKey = currentWeekKey
                mergedWeeklyXp = maxOf(local.weeklyXp, remote.weeklyXp).coerceIn(0, MAX_WEEKLY_XP)
                mergedWeeklyQ = maxOf(local.weeklyQuestions, remote.weeklyQuestions).coerceIn(0, MAX_WEEKLY_QUESTIONS)
                mergedWeeklyCorrect = maxOf(local.weeklyCorrect, remote.weeklyCorrect).coerceIn(0, MAX_WEEKLY_QUESTIONS)
                mergedWeeklySessions = maxOf(local.weeklySessions, remote.weeklySessions).coerceIn(0, MAX_WEEKLY_SESSIONS)
                mergedClaimed = (local.weeklyClaimedIds + remote.weeklyClaimedIds)
                    .filter { it in VALID_CHALLENGE_IDS }
                    .distinct()
                    .sorted()
                    .take(MAX_WEEKLY_CLAIMED_COUNT)
            }
            remoteWeek == currentWeekKey -> {
                mergedWeekKey = currentWeekKey
                mergedWeeklyXp = remote.weeklyXp.coerceIn(0, MAX_WEEKLY_XP)
                mergedWeeklyQ = remote.weeklyQuestions.coerceIn(0, MAX_WEEKLY_QUESTIONS)
                mergedWeeklyCorrect = remote.weeklyCorrect.coerceIn(0, MAX_WEEKLY_QUESTIONS)
                mergedWeeklySessions = remote.weeklySessions.coerceIn(0, MAX_WEEKLY_SESSIONS)
                mergedClaimed = remote.weeklyClaimedIds.filter { it in VALID_CHALLENGE_IDS }.distinct().sorted()
            }
            localWeek == currentWeekKey -> {
                mergedWeekKey = currentWeekKey
                mergedWeeklyXp = local.weeklyXp.coerceIn(0, MAX_WEEKLY_XP)
                mergedWeeklyQ = local.weeklyQuestions.coerceIn(0, MAX_WEEKLY_QUESTIONS)
                mergedWeeklyCorrect = local.weeklyCorrect.coerceIn(0, MAX_WEEKLY_QUESTIONS)
                mergedWeeklySessions = local.weeklySessions.coerceIn(0, MAX_WEEKLY_SESSIONS)
                mergedClaimed = local.weeklyClaimedIds.filter { it in VALID_CHALLENGE_IDS }.distinct().sorted()
            }
            else -> {
                mergedWeekKey = currentWeekKey
                mergedWeeklyXp = 0
                mergedWeeklyQ = 0
                mergedWeeklyCorrect = 0
                mergedWeeklySessions = 0
                mergedClaimed = emptyList()
            }
        }

        val localRecovery = StudyGoalLogic.sanitizeDateKey(local.lastStreakRecoveryDate)
        val remoteRecovery = StudyGoalLogic.sanitizeDateKey(remote.lastStreakRecoveryDate)
        val mergedLastMilestone = maxOf(local.lastShieldEarnedStreak, remote.lastShieldEarnedStreak)
            .coerceIn(0, StudyGoalLogic.MAX_COUNT_BOUND)
        val (mergedRecoveryDate, mergedShields) = when {
            remoteRecovery > localRecovery -> remoteRecovery to remote.streakShields.coerceIn(0, MAX_STREAK_SHIELDS)
            localRecovery > remoteRecovery -> localRecovery to local.streakShields.coerceIn(0, MAX_STREAK_SHIELDS)
            else -> localRecovery to maxOf(local.streakShields, remote.streakShields).coerceIn(0, MAX_STREAK_SHIELDS)
        }

        val mergedBadges = (local.unlockedBadgeIds + remote.unlockedBadgeIds)
            .filter { it in VALID_BADGE_IDS }
            .distinct()
            .sorted()
            .take(MAX_BADGES_COUNT)

        return GamificationState(
            totalXp = maxOf(local.totalXp, remote.totalXp).coerceIn(0, MAX_TOTAL_XP),
            weeklyXp = mergedWeeklyXp,
            weeklyKey = mergedWeekKey,
            weeklyQuestions = mergedWeeklyQ,
            weeklyCorrect = mergedWeeklyCorrect,
            weeklySessions = mergedWeeklySessions,
            weeklyClaimedIds = mergedClaimed,
            unlockedBadgeIds = mergedBadges,
            streakShields = mergedShields,
            lastShieldEarnedStreak = mergedLastMilestone,
            lastStreakRecoveryDate = mergedRecoveryDate,
            qotdAnsweredCount = maxOf(local.qotdAnsweredCount, remote.qotdAnsweredCount).coerceIn(0, MAX_COUNTER_BOUND),
            completedSessionsCount = maxOf(local.completedSessionsCount, remote.completedSessionsCount).coerceIn(0, MAX_COUNTER_BOUND),
            focusSprintsCount = maxOf(local.focusSprintsCount, remote.focusSprintsCount).coerceIn(0, MAX_COUNTER_BOUND),
            srsMasteredCount = maxOf(local.srsMasteredCount, remote.srsMasteredCount).coerceIn(0, MAX_COUNTER_BOUND),
            bestSessionAccuracy = maxOf(local.bestSessionAccuracy, remote.bestSessionAccuracy).coerceIn(0, 100),
            dailyAwardDateKey = local.dailyAwardDateKey,
            dailyAwardedQuestionIds = local.dailyAwardedQuestionIds,
            lastDailyGoalBonusDate = local.lastDailyGoalBonusDate
        )
    }

    fun serialize(state: GamificationState): String {
        return buildString {
            append("totalXp=").append(state.totalXp).append('\n')
            append("weeklyXp=").append(state.weeklyXp).append('\n')
            append("weeklyKey=").append(state.weeklyKey).append('\n')
            append("weeklyQuestions=").append(state.weeklyQuestions).append('\n')
            append("weeklyCorrect=").append(state.weeklyCorrect).append('\n')
            append("weeklySessions=").append(state.weeklySessions).append('\n')
            append("weeklyClaimedIds=").append(state.weeklyClaimedIds.joinToString(",")).append('\n')
            append("unlockedBadgeIds=").append(state.unlockedBadgeIds.joinToString(",")).append('\n')
            append("streakShields=").append(state.streakShields).append('\n')
            append("lastShieldEarnedStreak=").append(state.lastShieldEarnedStreak).append('\n')
            append("lastStreakRecoveryDate=").append(state.lastStreakRecoveryDate).append('\n')
            append("qotdAnsweredCount=").append(state.qotdAnsweredCount).append('\n')
            append("completedSessionsCount=").append(state.completedSessionsCount).append('\n')
            append("focusSprintsCount=").append(state.focusSprintsCount).append('\n')
            append("srsMasteredCount=").append(state.srsMasteredCount).append('\n')
            append("bestSessionAccuracy=").append(state.bestSessionAccuracy).append('\n')
            append("dailyAwardDateKey=").append(state.dailyAwardDateKey).append('\n')
            append("dailyAwardedQuestionIds=").append(state.dailyAwardedQuestionIds.joinToString(",")).append('\n')
            append("lastDailyGoalBonusDate=").append(state.lastDailyGoalBonusDate)
        }
    }

    fun deserialize(raw: String?): GamificationState {
        if (raw.isNullOrBlank()) return GamificationState()
        val map = mutableMapOf<String, String>()
        raw.lineSequence().forEach { line ->
            val idx = line.indexOf('=')
            if (idx > 0) {
                map[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
            }
        }
        fun intVal(key: String, max: Int): Int =
            (map[key]?.toIntOrNull() ?: 0).coerceIn(0, max)

        fun listVal(key: String, allowed: Set<String>? = null, max: Int): List<String> {
            val rawList = map[key].orEmpty()
            if (rawList.isBlank()) return emptyList()
            return rawList.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && (allowed == null || it in allowed) }
                .distinct()
                .take(max)
        }

        return GamificationState(
            totalXp = intVal("totalXp", MAX_TOTAL_XP),
            weeklyXp = intVal("weeklyXp", MAX_WEEKLY_XP),
            weeklyKey = sanitizeWeekKey(map["weeklyKey"].orEmpty()),
            weeklyQuestions = intVal("weeklyQuestions", MAX_WEEKLY_QUESTIONS),
            weeklyCorrect = intVal("weeklyCorrect", MAX_WEEKLY_QUESTIONS),
            weeklySessions = intVal("weeklySessions", MAX_WEEKLY_SESSIONS),
            weeklyClaimedIds = listVal("weeklyClaimedIds", VALID_CHALLENGE_IDS, MAX_WEEKLY_CLAIMED_COUNT),
            unlockedBadgeIds = listVal("unlockedBadgeIds", VALID_BADGE_IDS, MAX_BADGES_COUNT),
            streakShields = intVal("streakShields", MAX_STREAK_SHIELDS),
            lastShieldEarnedStreak = intVal("lastShieldEarnedStreak", StudyGoalLogic.MAX_COUNT_BOUND),
            lastStreakRecoveryDate = StudyGoalLogic.sanitizeDateKey(map["lastStreakRecoveryDate"].orEmpty()),
            qotdAnsweredCount = intVal("qotdAnsweredCount", MAX_COUNTER_BOUND),
            completedSessionsCount = intVal("completedSessionsCount", MAX_COUNTER_BOUND),
            focusSprintsCount = intVal("focusSprintsCount", MAX_COUNTER_BOUND),
            srsMasteredCount = intVal("srsMasteredCount", MAX_COUNTER_BOUND),
            bestSessionAccuracy = intVal("bestSessionAccuracy", 100),
            dailyAwardDateKey = StudyGoalLogic.sanitizeDateKey(map["dailyAwardDateKey"].orEmpty()),
            dailyAwardedQuestionIds = listVal("dailyAwardedQuestionIds", null, MAX_DAILY_DEDUP_IDS),
            lastDailyGoalBonusDate = StudyGoalLogic.sanitizeDateKey(map["lastDailyGoalBonusDate"].orEmpty())
        )
    }
}
