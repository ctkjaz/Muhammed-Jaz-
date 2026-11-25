package com.example

import com.example.data.model.DailyFocusProgress
import com.example.data.model.Flashcard
import com.example.data.model.FocusTimerLogic
import com.example.data.model.FocusTimerPreset
import com.example.data.model.RapidRecallCatalog
import com.example.data.model.RapidRecallDeckCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V30FeaturesLogicTest {

    @Test
    fun rapidRecallCatalog_has30ValidUniqueClinicalCardsAcrossAll5ClinicalCategories() {
        val cards = RapidRecallCatalog.BUILTIN_CLINICAL_CARDS
        assertEquals(30, cards.size)
        assertEquals(cards.size, cards.map { it.id }.toSet().size)

        val expectedCategories = setOf(
            RapidRecallDeckCategory.DRUGS_DOC,
            RapidRecallDeckCategory.IMMUNIZATION,
            RapidRecallDeckCategory.PROCEDURES,
            RapidRecallDeckCategory.CLINICAL_SIGNS,
            RapidRecallDeckCategory.OBG_PEDS
        )
        assertEquals(expectedCategories, cards.map { it.category }.toSet())

        cards.forEach { c ->
            assertTrue("Prompt blank for ${c.id}", c.frontPrompt.length >= 20)
            assertTrue("Answer blank for ${c.id}", c.backAnswer.length >= 15)
            assertTrue("Pearl too short for ${c.id}", c.examPearl.length >= 20)
        }
    }

    @Test
    fun rapidRecallCatalog_unifiesCaFlashcardsAndFiltersMasteryCleanly() {
        val ca = listOf(
            Flashcard(id = 1, title = "U-WIN Portal", tag = "Health-Prog", body = "Digital immunization registry under UIP India."),
            Flashcard(id = 2, title = "KAVACH ATP", tag = "Railways", body = "Indigenous Indian Railways Automatic Train Protection.")
        )
        val deck = RapidRecallCatalog.buildUnifiedDeck(ca)
        assertEquals(32, deck.size)
        assertEquals(2, deck.count { it.category == RapidRecallDeckCategory.CA_GK })

        val mastered = setOf("RR_DOC_01", "RR_NIS_01")
        val unmasteredOnly = RapidRecallCatalog.filterDeck(
            deck = deck,
            masteredIds = mastered,
            unmasteredOnly = true
        )
        assertEquals(30, unmasteredOnly.size)
        assertFalse(unmasteredOnly.any { it.id in mastered })

        val searchMg = RapidRecallCatalog.filterDeck(deck, query = "Pritchard")
        assertEquals(1, searchMg.size)
        assertEquals("RR_DOC_01", searchMg.first().id)

        val encoded = RapidRecallCatalog.serializeMasteredIds(mastered)
        val decoded = RapidRecallCatalog.deserializeMasteredIds(encoded)
        assertEquals(mastered, decoded)
    }

    @Test
    fun focusTimerLogic_formatsClockAndTracksDailyStudyMinutesWithBreakExclusion() {
        assertEquals("25:00", FocusTimerLogic.formatClock(25 * 60))
        assertEquals("00:05", FocusTimerLogic.formatClock(5))
        assertEquals("1:30:00", FocusTimerLogic.formatClock(90 * 60))

        val day1 = "2026-11-25"
        val day2 = "2026-11-26"
        var progress = DailyFocusProgress()
        progress = FocusTimerLogic.recordCompletedSession(progress, FocusTimerPreset.POMODORO_25, todayKey = day1)
        assertEquals(25, progress.effectiveMinutes(day1))
        assertEquals(1, progress.effectiveSessions(day1))

        // Breaks do not inflate study minutes or sprint counts
        progress = FocusTimerLogic.recordCompletedSession(progress, FocusTimerPreset.SHORT_BREAK_5, todayKey = day1)
        assertEquals(25, progress.effectiveMinutes(day1))
        assertEquals(1, progress.effectiveSessions(day1))

        // Round-trip serialization
        val encoded = FocusTimerLogic.serializeProgress(progress)
        val decoded = FocusTimerLogic.deserializeProgress(encoded)
        assertEquals(progress, decoded)

        // New day resets cleanly
        assertEquals(0, decoded.effectiveMinutes(day2))
        val nextDay = FocusTimerLogic.recordCompletedSession(decoded, FocusTimerPreset.DEEP_STUDY_50, todayKey = day2)
        assertEquals(50, nextDay.effectiveMinutes(day2))
        assertEquals(1, nextDay.effectiveSessions(day2))
    }
}
