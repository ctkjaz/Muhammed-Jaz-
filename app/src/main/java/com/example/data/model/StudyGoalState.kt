package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyStudyGoalState(
    val dailyGoalQuestions: Int = StudyGoalLogic.DEFAULT_DAILY_GOAL,
    val todayStudyDate: String = "",
    val todayAnsweredCount: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastGoalMetDate: String = ""
) {
    fun effectiveTodayCount(todayKey: String = StudyGoalLogic.todayDateKey()): Int =
        if (todayStudyDate == todayKey) todayAnsweredCount.coerceAtLeast(0) else 0

    fun effectiveCurrentStreak(
        todayKey: String = StudyGoalLogic.todayDateKey(),
        yesterdayKey: String = StudyGoalLogic.yesterdayDateKey()
    ): Int = when (lastGoalMetDate) {
        todayKey, yesterdayKey -> currentStreakDays.coerceAtLeast(0)
        else -> 0
    }

    fun remainingQuestions(todayKey: String = StudyGoalLogic.todayDateKey()): Int =
        (dailyGoalQuestions - effectiveTodayCount(todayKey)).coerceAtLeast(0)

    fun isGoalMetToday(todayKey: String = StudyGoalLogic.todayDateKey()): Boolean =
        effectiveTodayCount(todayKey) >= dailyGoalQuestions.coerceAtLeast(1)

    fun canRecoverStreakToday(
        todayKey: String = StudyGoalLogic.todayDateKey(),
        twoDaysAgoKey: String = StudyGoalLogic.twoDaysAgoDateKey()
    ): Boolean = lastGoalMetDate == twoDaysAgoKey && currentStreakDays > 0 && lastGoalMetDate != todayKey

    fun progressFraction(todayKey: String = StudyGoalLogic.todayDateKey()): Float =
        (effectiveTodayCount(todayKey).toFloat() / dailyGoalQuestions.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)
}

data class StudyActivityResult(
    val updatedState: DailyStudyGoalState,
    val justMetDailyGoal: Boolean,
    val usedStreakRecovery: Boolean = false
)

object StudyGoalLogic {
    const val MIN_DAILY_GOAL = 5
    const val MAX_DAILY_GOAL = 200
    const val DEFAULT_DAILY_GOAL = 25
    const val MAX_COUNT_BOUND = 10000

    val DAILY_GOAL_PRESETS: List<Int> = listOf(15, 25, 50, 100)

    fun todayDateKey(nowMs: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date(nowMs))
    }

    fun yesterdayDateKey(nowMs: Long = System.currentTimeMillis()): String {
        return daysAgoDateKey(1, nowMs)
    }

    fun twoDaysAgoDateKey(nowMs: Long = System.currentTimeMillis()): String {
        return daysAgoDateKey(2, nowMs)
    }

    fun daysAgoDateKey(daysAgo: Int, nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(cal.time)
    }

    fun sanitizeDateKey(raw: String): String {
        val clean = raw.trim().take(16)
        return if (clean.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) clean else ""
    }

    fun recordStudyActivity(
        current: DailyStudyGoalState,
        answeredDelta: Int,
        todayKey: String = todayDateKey(),
        yesterdayKey: String = yesterdayDateKey(),
        twoDaysAgoKey: String = twoDaysAgoDateKey(),
        allowStreakRecovery: Boolean = false
    ): StudyActivityResult {
        val safeGoal = current.dailyGoalQuestions.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)
        val priorTodayCount = if (current.todayStudyDate == todayKey) {
            current.todayAnsweredCount.coerceIn(0, MAX_COUNT_BOUND)
        } else {
            0
        }
        val safeDelta = answeredDelta.coerceAtLeast(0)
        val newTodayCount = (priorTodayCount + safeDelta).coerceIn(0, MAX_COUNT_BOUND)
        val alreadyMetToday = current.lastGoalMetDate == todayKey
        val nowMetToday = newTodayCount >= safeGoal

        return if (nowMetToday && !alreadyMetToday) {
            val recoveredWithShield = allowStreakRecovery &&
                current.lastGoalMetDate == twoDaysAgoKey &&
                current.currentStreakDays > 0
            val continuingStreak = when {
                current.lastGoalMetDate == yesterdayKey -> current.currentStreakDays.coerceIn(0, MAX_COUNT_BOUND)
                recoveredWithShield -> current.currentStreakDays.coerceIn(0, MAX_COUNT_BOUND)
                else -> 0
            }
            val newStreak = (continuingStreak + 1).coerceIn(1, MAX_COUNT_BOUND)
            val newLongest = maxOf(current.longestStreakDays.coerceIn(0, MAX_COUNT_BOUND), newStreak)
            StudyActivityResult(
                updatedState = DailyStudyGoalState(
                    dailyGoalQuestions = safeGoal,
                    todayStudyDate = todayKey,
                    todayAnsweredCount = newTodayCount,
                    currentStreakDays = newStreak,
                    longestStreakDays = newLongest,
                    lastGoalMetDate = todayKey
                ),
                justMetDailyGoal = true,
                usedStreakRecovery = recoveredWithShield
            )
        } else {
            val activeStreak = when {
                current.lastGoalMetDate == todayKey || current.lastGoalMetDate == yesterdayKey ->
                    current.currentStreakDays.coerceIn(0, MAX_COUNT_BOUND)
                allowStreakRecovery && current.lastGoalMetDate == twoDaysAgoKey ->
                    current.currentStreakDays.coerceIn(0, MAX_COUNT_BOUND)
                else -> 0
            }
            val safeLongest = maxOf(current.longestStreakDays.coerceIn(0, MAX_COUNT_BOUND), activeStreak)
            StudyActivityResult(
                updatedState = DailyStudyGoalState(
                    dailyGoalQuestions = safeGoal,
                    todayStudyDate = todayKey,
                    todayAnsweredCount = newTodayCount,
                    currentStreakDays = activeStreak,
                    longestStreakDays = safeLongest,
                    lastGoalMetDate = current.lastGoalMetDate
                ),
                justMetDailyGoal = false,
                usedStreakRecovery = false
            )
        }
    }

    fun updateDailyGoalTarget(
        current: DailyStudyGoalState,
        newTarget: Int,
        todayKey: String = todayDateKey(),
        yesterdayKey: String = yesterdayDateKey()
    ): StudyActivityResult {
        val safeTarget = newTarget.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)
        val updatedGoal = current.copy(dailyGoalQuestions = safeTarget)
        val todayCount = updatedGoal.effectiveTodayCount(todayKey)
        return if (todayCount >= safeTarget && updatedGoal.lastGoalMetDate != todayKey) {
            recordStudyActivity(updatedGoal, answeredDelta = 0, todayKey = todayKey, yesterdayKey = yesterdayKey)
        } else {
            StudyActivityResult(updatedState = updatedGoal, justMetDailyGoal = false)
        }
    }

    fun mergeCloudGoalState(
        local: DailyStudyGoalState,
        remote: DailyStudyGoalState,
        todayKey: String = todayDateKey()
    ): DailyStudyGoalState {
        val safeGoal = remote.dailyGoalQuestions.coerceIn(MIN_DAILY_GOAL, MAX_DAILY_GOAL)

        val localToday = if (local.todayStudyDate == todayKey) local.todayAnsweredCount.coerceIn(0, MAX_COUNT_BOUND) else -1
        val remoteToday = if (remote.todayStudyDate == todayKey) remote.todayAnsweredCount.coerceIn(0, MAX_COUNT_BOUND) else -1
        val (mergedTodayDate, mergedTodayCount) = when {
            localToday >= 0 && remoteToday >= 0 -> todayKey to maxOf(localToday, remoteToday)
            localToday >= 0 -> todayKey to localToday
            remoteToday >= 0 -> todayKey to remoteToday
            remote.todayStudyDate >= local.todayStudyDate ->
                sanitizeDateKey(remote.todayStudyDate) to remote.todayAnsweredCount.coerceIn(0, MAX_COUNT_BOUND)
            else ->
                sanitizeDateKey(local.todayStudyDate) to local.todayAnsweredCount.coerceIn(0, MAX_COUNT_BOUND)
        }

        val cleanLocalMet = sanitizeDateKey(local.lastGoalMetDate)
        val cleanRemoteMet = sanitizeDateKey(remote.lastGoalMetDate)
        val (mergedLastMetDate, mergedCurrentStreak) = when {
            cleanLocalMet == cleanRemoteMet -> cleanLocalMet to maxOf(
                local.currentStreakDays.coerceIn(0, MAX_COUNT_BOUND),
                remote.currentStreakDays.coerceIn(0, MAX_COUNT_BOUND)
            )
            cleanRemoteMet > cleanLocalMet -> cleanRemoteMet to remote.currentStreakDays.coerceIn(0, MAX_COUNT_BOUND)
            else -> cleanLocalMet to local.currentStreakDays.coerceIn(0, MAX_COUNT_BOUND)
        }

        val mergedLongest = maxOf(
            local.longestStreakDays.coerceIn(0, MAX_COUNT_BOUND),
            remote.longestStreakDays.coerceIn(0, MAX_COUNT_BOUND),
            mergedCurrentStreak
        )

        return DailyStudyGoalState(
            dailyGoalQuestions = safeGoal,
            todayStudyDate = mergedTodayDate,
            todayAnsweredCount = mergedTodayCount,
            currentStreakDays = mergedCurrentStreak,
            longestStreakDays = mergedLongest,
            lastGoalMetDate = mergedLastMetDate
        )
    }
}
