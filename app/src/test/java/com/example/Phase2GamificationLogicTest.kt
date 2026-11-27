package com.example

import com.example.data.model.DailyStudyGoalState
import com.example.data.model.GamificationLogic
import com.example.data.model.GamificationState
import com.example.data.model.StudyGoalLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2GamificationLogicTest {

    @Test
    fun xpAwarding_correctAndIncorrectWithSameDayAntiFarmingDedup() {
        val weekKey = "2026-W48"
        val today = "2026-11-27"

        val first = GamificationLogic.recordQuestionAnswer(
            current = GamificationState(),
            questionId = "q_101",
            isCorrect = true,
            lifetimeAnsweredCount = 1,
            longestStreakDays = 0,
            todayKey = today,
            weekKey = weekKey
        )
        assertEquals(GamificationLogic.XP_CORRECT_ANSWER, first.xpAwarded)
        assertEquals(10, first.updatedState.totalXp)
        assertEquals(1, first.updatedState.weeklyQuestions)
        assertEquals(1, first.updatedState.weeklyCorrect)

        // Repeat same question same day -> anti-farming yields 0 XP
        val duplicate = GamificationLogic.recordQuestionAnswer(
            current = first.updatedState,
            questionId = "q_101",
            isCorrect = true,
            lifetimeAnsweredCount = 2,
            longestStreakDays = 0,
            todayKey = today,
            weekKey = weekKey
        )
        assertEquals(0, duplicate.xpAwarded)
        assertEquals(10, duplicate.updatedState.totalXp)

        // Different question answered wrong -> honest attempt XP (+2)
        val wrong = GamificationLogic.recordQuestionAnswer(
            current = duplicate.updatedState,
            questionId = "q_102",
            isCorrect = false,
            lifetimeAnsweredCount = 2,
            longestStreakDays = 0,
            todayKey = today,
            weekKey = weekKey
        )
        assertEquals(GamificationLogic.XP_INCORRECT_ATTEMPT, wrong.xpAwarded)
        assertEquals(12, wrong.updatedState.totalXp)
        assertEquals(2, wrong.updatedState.weeklyQuestions)
        assertEquals(1, wrong.updatedState.weeklyCorrect)
    }

    @Test
    fun levelAndRankCurve_advancesSmoothlyAndCapsSafely() {
        val lv1 = GamificationLogic.computeLevelProgress(0)
        assertEquals(1, lv1.level)
        assertEquals("Nursing Intern", lv1.rankTitle)
        assertEquals(0, lv1.xpIntoLevel)
        assertEquals(150, lv1.xpForNextLevel)

        // 150 XP -> Level 2; 150 + 200 = 350 XP -> Level 3 (Ward Staff Nurse)
        val lv3 = GamificationLogic.computeLevelProgress(360)
        assertEquals(3, lv3.level)
        assertEquals("Ward Staff Nurse", lv3.rankTitle)
        assertEquals(10, lv3.xpIntoLevel)
        assertEquals(250, lv3.xpForNextLevel)

        val capped = GamificationLogic.computeLevelProgress(9_999_999)
        assertEquals(GamificationLogic.MAX_LEVEL, capped.level)
        assertEquals("Chief Nursing Mentor", capped.rankTitle)
        assertEquals(1f, capped.progressFraction, 0.001f)
    }

    @Test
    fun batchCbtSession_awards20QBonusAndUnlocksExamBadges() {
        val results = (1..20).map { idx -> "cbt_q_$idx" to (idx <= 17) } // 85% accuracy
        val res = GamificationLogic.recordBatchSessionCompleted(
            current = GamificationState(),
            results = results,
            accuracyPercent = 85,
            lifetimeAnsweredCount = 20,
            longestStreakDays = 1,
            todayKey = "2026-11-27",
            weekKey = "2026-W48"
        )
        // 17*10 + 3*2 + 25 session bonus = 201 XP -> also levels up from Lv 1 to Lv 2!
        assertEquals(201, res.xpAwarded)
        assertEquals(2, res.leveledUpTo)
        assertEquals(1, res.updatedState.completedSessionsCount)
        assertEquals(85, res.updatedState.bestSessionAccuracy)
        assertTrue(GamificationLogic.BADGE_FIRST_STEPS in res.updatedState.unlockedBadgeIds)
        assertTrue(GamificationLogic.BADGE_MOCK_FINISHER in res.updatedState.unlockedBadgeIds)
        assertTrue(GamificationLogic.BADGE_SHARPSHOOTER in res.updatedState.unlockedBadgeIds)
    }

    @Test
    fun weeklyChallenges_claimAutomaticallyOnceAndResetOnNewWeek() {
        val base = GamificationState(
            totalXp = 500,
            weeklyXp = 200,
            weeklyKey = "2026-W48",
            weeklyQuestions = 99,
            weeklyCorrect = 49,
            weeklySessions = 2
        )
        // Answering 1 more right question hits both 100Q (+60 XP) and 50 Correct (+50 XP)
        val res = GamificationLogic.recordQuestionAnswer(
            current = base,
            questionId = "q_100",
            isCorrect = true,
            lifetimeAnsweredCount = 100,
            longestStreakDays = 5,
            todayKey = "2026-11-27",
            weekKey = "2026-W48"
        )
        assertEquals(10 + 60 + 50, res.xpAwarded)
        assertEquals(2, res.newlyClaimedChallenges.size)
        assertTrue(GamificationLogic.CHALLENGE_WEEKLY_100Q in res.updatedState.weeklyClaimedIds)
        assertTrue(GamificationLogic.CHALLENGE_WEEKLY_50_CORRECT in res.updatedState.weeklyClaimedIds)
        assertTrue(GamificationLogic.BADGE_CENTURION in res.updatedState.unlockedBadgeIds)

        // New week rollover resets weekly counters cleanly while preserving total XP & badges
        val rolled = GamificationLogic.normalizeForWeek(res.updatedState, "2026-W49")
        assertEquals("2026-W49", rolled.weeklyKey)
        assertEquals(0, rolled.weeklyQuestions)
        assertEquals(0, rolled.weeklyXp)
        assertTrue(rolled.weeklyClaimedIds.isEmpty())
        assertEquals(res.updatedState.totalXp, rolled.totalXp)
        assertEquals(res.updatedState.unlockedBadgeIds, rolled.unlockedBadgeIds)
    }

    @Test
    fun fairStreakShield_earnsAt7DaysRecoversSingleMissedDayAndRejectsTwoDayMiss() {
        // 1. Reach 7-day streak -> earns +50 daily goal + +75 7-day bonus + 1 free shield
        val earned = GamificationLogic.recordDailyGoalAndStreakEvent(
            current = GamificationState(),
            justMetDailyGoal = true,
            usedStreakRecovery = false,
            currentStreakDays = 7,
            longestStreakDays = 7,
            lifetimeAnsweredCount = 150,
            todayKey = "2026-11-25",
            weekKey = "2026-W48"
        )
        assertTrue(earned.earnedStreakShield)
        assertEquals(1, earned.updatedState.streakShields)
        assertEquals(GamificationLogic.XP_DAILY_GOAL_BONUS + GamificationLogic.XP_STREAK_7DAY_BONUS, earned.xpAwarded)
        assertTrue(GamificationLogic.BADGE_STREAK_7 in earned.updatedState.unlockedBadgeIds)

        // 2. Miss exactly 1 day (lastGoalMetDate = 2026-11-25, today = 2026-11-27, twoDaysAgo = 2026-11-25)
        val goalBefore = DailyStudyGoalState(
            dailyGoalQuestions = 10,
            todayStudyDate = "2026-11-25",
            todayAnsweredCount = 10,
            currentStreakDays = 7,
            longestStreakDays = 7,
            lastGoalMetDate = "2026-11-25"
        )
        assertTrue(goalBefore.canRecoverStreakToday(todayKey = "2026-11-27", twoDaysAgoKey = "2026-11-25"))

        val recoveredGoal = StudyGoalLogic.recordStudyActivity(
            current = goalBefore,
            answeredDelta = 10,
            todayKey = "2026-11-27",
            yesterdayKey = "2026-11-26",
            twoDaysAgoKey = "2026-11-25",
            allowStreakRecovery = true
        )
        assertTrue(recoveredGoal.justMetDailyGoal)
        assertTrue(recoveredGoal.usedStreakRecovery)
        assertEquals(8, recoveredGoal.updatedState.currentStreakDays)

        val afterSpend = GamificationLogic.recordDailyGoalAndStreakEvent(
            current = earned.updatedState,
            justMetDailyGoal = true,
            usedStreakRecovery = true,
            currentStreakDays = 8,
            longestStreakDays = 8,
            lifetimeAnsweredCount = 160,
            todayKey = "2026-11-27",
            weekKey = "2026-W48"
        )
        assertEquals(0, afterSpend.updatedState.streakShields)
        assertEquals("2026-11-27", afterSpend.updatedState.lastStreakRecoveryDate)

        // 3. Miss 2+ full days (lastGoalMetDate = 2026-11-24, today = 2026-11-27) -> shield NOT used, streak resets to 1
        val missedTwoDays = StudyGoalLogic.recordStudyActivity(
            current = goalBefore.copy(lastGoalMetDate = "2026-11-24"),
            answeredDelta = 10,
            todayKey = "2026-11-27",
            yesterdayKey = "2026-11-26",
            twoDaysAgoKey = "2026-11-25",
            allowStreakRecovery = true
        )
        assertFalse(missedTwoDays.usedStreakRecovery)
        assertEquals(1, missedTwoDays.updatedState.currentStreakDays)
    }

    @Test
    fun serializeDeserializeAndCloudMerge_preserveMonotonicProgressAndUnionBadges() {
        val local = GamificationState(
            totalXp = 620,
            weeklyXp = 180,
            weeklyKey = "2026-W48",
            weeklyQuestions = 45,
            weeklyCorrect = 38,
            weeklySessions = 2,
            weeklyClaimedIds = listOf(GamificationLogic.CHALLENGE_WEEKLY_3_SESSIONS),
            unlockedBadgeIds = listOf(GamificationLogic.BADGE_FIRST_STEPS, GamificationLogic.BADGE_QOTD_5),
            streakShields = 1,
            lastShieldEarnedStreak = 7,
            lastStreakRecoveryDate = "",
            qotdAnsweredCount = 5,
            completedSessionsCount = 2,
            focusSprintsCount = 3,
            srsMasteredCount = 4,
            bestSessionAccuracy = 84,
            dailyAwardDateKey = "2026-11-27",
            dailyAwardedQuestionIds = listOf("q_1", "q_2"),
            lastDailyGoalBonusDate = "2026-11-27"
        )
        val roundTrip = GamificationLogic.deserialize(GamificationLogic.serialize(local))
        assertEquals(local, roundTrip)

        val remote = GamificationState(
            totalXp = 850,
            weeklyXp = 210,
            weeklyKey = "2026-W48",
            weeklyQuestions = 60,
            weeklyCorrect = 30,
            weeklySessions = 1,
            weeklyClaimedIds = listOf(GamificationLogic.CHALLENGE_WEEKLY_50_CORRECT),
            unlockedBadgeIds = listOf(GamificationLogic.BADGE_CENTURION),
            streakShields = 0,
            lastShieldEarnedStreak = 7,
            lastStreakRecoveryDate = "2026-11-27",
            qotdAnsweredCount = 3,
            completedSessionsCount = 4,
            focusSprintsCount = 1,
            srsMasteredCount = 6,
            bestSessionAccuracy = 92
        )
        val merged = GamificationLogic.mergeWithCloud(local, remote, currentWeekKey = "2026-W48")
        assertEquals(850, merged.totalXp)
        assertEquals(210, merged.weeklyXp)
        assertEquals(60, merged.weeklyQuestions)
        assertEquals(38, merged.weeklyCorrect)
        assertEquals(2, merged.weeklySessions)
        assertEquals(
            setOf(
                GamificationLogic.BADGE_FIRST_STEPS,
                GamificationLogic.BADGE_QOTD_5,
                GamificationLogic.BADGE_CENTURION
            ),
            merged.unlockedBadgeIds.toSet()
        )
        assertEquals(0, merged.streakShields) // newer remote recovery date wins so shield isn't duplicated
        assertEquals("2026-11-27", merged.lastStreakRecoveryDate)
        assertEquals(92, merged.bestSessionAccuracy)
        assertEquals(listOf("q_1", "q_2"), merged.dailyAwardedQuestionIds)
        assertNotNull(GamificationLogic.ALL_BADGES.firstOrNull { it.id == GamificationLogic.BADGE_DEEP_FOCUS_3 })
        assertNull(GamificationLogic.recordTimedPracticeCompleted(local, 10, 90, 50, 7, "2026-W48").leveledUpTo)
    }
}
