package com.example

import com.example.data.model.Question
import com.example.data.model.QuestionDifficulty
import com.example.data.model.QuestionItem
import com.example.data.model.QuestionProgress
import com.example.data.model.SmartWeakTopicPlanner
import com.example.data.model.SrsCardState
import com.example.data.model.SrsStage
import com.example.data.model.TopicWeaknessSeverity
import com.example.data.notifications.AnnouncementCategory
import com.example.data.notifications.AnnouncementPriority
import com.example.data.notifications.CloudAnnouncement
import com.example.data.notifications.EngagementLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V26FeaturesLogicTest {

    private fun makeQuestionItem(
        id: String,
        subject: String,
        attempts: Int,
        correct: Int
    ): QuestionItem {
        val q = Question(
            id = id,
            category = "pyq",
            shiftYear = "PYQ 2026",
            subject = subject,
            questionText = "Clinical scenario $id",
            options = listOf("Option A", "Option B", "Option C", "Option D"),
            correctIndex = 0,
            explanation = "Rationale $id",
            difficulty = QuestionDifficulty.MEDIUM
        )
        val p = QuestionProgress(
            questionId = id,
            attemptsCount = attempts,
            correctCount = correct,
            wrongCount = (attempts - correct).coerceAtLeast(0),
            lastOutcome = when {
                attempts == 0 -> null
                correct < attempts -> "wrong"
                else -> "correct"
            }
        )
        return QuestionItem(q, p)
    }

    @Test
    fun smartWeakTopicPlannerRanksCriticalLowestAccuracyFirstAndBuilds7DaySchedule() {
        val nowMs = 1_760_000_000_000L
        val questions = listOf(
            // PHARM: 25% accuracy (CRITICAL)
            makeQuestionItem("q_ph1", "PHARM", attempts = 4, correct = 1),
            makeQuestionItem("q_ph2", "PHARM", attempts = 4, correct = 1),
            // OBG: 60% accuracy + 2 due SRS cards (NEEDS_WORK)
            makeQuestionItem("q_ob1", "OBG", attempts = 5, correct = 3),
            makeQuestionItem("q_ob2", "OBG", attempts = 5, correct = 3),
            // MSN: 90% accuracy (STRONG)
            makeQuestionItem("q_ms1", "MSN", attempts = 10, correct = 9),
            // CHN: Untouched
            makeQuestionItem("q_ch1", "CHN", attempts = 0, correct = 0)
        )
        val srsCards = mapOf(
            "q_ob1" to SrsCardState(
                questionId = "q_ob1",
                stage = SrsStage.BOX_1D,
                nextReviewAtMs = nowMs - 3_600_000L,
                lastReviewedAtMs = nowMs - 90_000_000L,
                consecutiveCorrect = 0
            ),
            "q_ob2" to SrsCardState(
                questionId = "q_ob2",
                stage = SrsStage.BOX_1D,
                nextReviewAtMs = nowMs - 1_800_000L,
                lastReviewedAtMs = nowMs - 90_000_000L,
                consecutiveCorrect = 0
            )
        )

        val plan = SmartWeakTopicPlanner.buildPlan(
            questions = questions,
            srsCards = srsCards,
            dailyGoalQuestions = 30,
            nowMs = nowMs
        )

        assertEquals(4, plan.diagnostics.size)
        assertEquals("PHARM", plan.diagnostics.first().subjectCode)
        assertEquals(TopicWeaknessSeverity.CRITICAL, plan.diagnostics.first().severity)
        assertEquals(25, plan.diagnostics.first().accuracyPercent)

        assertEquals("OBG", plan.diagnostics[1].subjectCode)
        assertEquals(TopicWeaknessSeverity.NEEDS_WORK, plan.diagnostics[1].severity)
        assertEquals(2, plan.diagnostics[1].dueSrsCount)

        assertEquals(7, plan.sevenDayPlan.size)
        assertEquals("PHARM", plan.sevenDayPlan.first().subjectCode)
        assertEquals("Day 1 · Today", plan.sevenDayPlan.first().dayLabel)
        // Strong subjects should not displace weak/untouched subjects in the recovery rotation
        assertTrue(plan.sevenDayPlan.none { it.subjectCode == "MSN" })
    }

    @Test
    fun selectWeightedAutoDrillPrioritizesDueSrsThenStruggledThenWeakSubjectsWithoutDuplicates() {
        val nowMs = 1_760_000_000_000L
        val questions = listOf(
            makeQuestionItem("q_due", "OBG", attempts = 2, correct = 1),
            makeQuestionItem("q_miss", "PHARM", attempts = 3, correct = 0),
            makeQuestionItem("q_weak_new", "PHARM", attempts = 0, correct = 0),
            makeQuestionItem("q_strong", "MSN", attempts = 5, correct = 5)
        )
        val srs = mapOf(
            "q_due" to SrsCardState(
                questionId = "q_due",
                stage = SrsStage.BOX_1D,
                nextReviewAtMs = nowMs - 10_000L,
                lastReviewedAtMs = nowMs - 86_400_000L,
                consecutiveCorrect = 0
            )
        )

        val picked = SmartWeakTopicPlanner.selectWeightedAutoDrill(
            questions = questions,
            srsCards = srs,
            targetCount = 3,
            nowMs = nowMs
        )

        assertEquals(3, picked.size)
        assertEquals(3, picked.map { it.id }.toSet().size)
        assertEquals("q_due", picked[0].id)
        assertEquals("q_miss", picked[1].id)
        assertEquals("q_weak_new", picked[2].id)

        val scoped = SmartWeakTopicPlanner.selectWeightedAutoDrill(
            questions = questions,
            srsCards = srs,
            targetCount = 5,
            targetSubjectCode = "PHARM",
            nowMs = nowMs
        )
        assertEquals(2, scoped.size)
        assertTrue(scoped.all { it.question.subject == "PHARM" })
    }

    @Test
    fun appUpdateAnnouncementMatchesAllActiveExamsForBackgroundPush() {
        val updateAnn = CloudAnnouncement(
            announcementId = "ann_upd_260",
            title = "ClinEdge v2.6.0 Available",
            body = "Background push notifications and Smart Weak-Topic Auto-Drill are live!",
            category = AnnouncementCategory.APP_UPDATE.dbValue,
            targetExamId = "all",
            priority = AnnouncementPriority.URGENT.dbValue
        )
        assertTrue(EngagementLogic.announcementMatchesExam(updateAnn, "aiims_norcet"))
        assertTrue(EngagementLogic.announcementMatchesExam(updateAnn, "rrb_nursing"))
        assertTrue(EngagementLogic.announcementMatchesExam(updateAnn, "pgimer_nursing"))
    }
}
